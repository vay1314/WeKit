//! JNI entry points

#![allow(non_snake_case)]

mod crash_handler;
mod crash_triggerer;
mod logging;
mod shared;
mod utils;

use std::{
    ffi::CString,
    io::{BufRead, BufReader},
};

use crash_handler::{install_crash_handler, uninstall_crash_handler};
use crash_triggerer::trigger_test_crash;

use goblin::elf::Elf;
use jni::sys::{
    JNI_FALSE, JNI_TRUE, JNI_VERSION_1_6, JNIEnv as RawJNIEnv, JavaVM, jboolean, jint, jlong,
    jobject, jstring,
};
use libc::c_void;

use crate::utils::with_jstring;

// ─────────────────────────────────────────────────────────────────────────────
// JNI exports
// ─────────────────────────────────────────────────────────────────────────────

/// Install the native crash handler.
///
/// Java signature: `(Ljava/lang/String;)Z`
#[unsafe(no_mangle)]
pub extern "C" fn Java_moe_ouom_wekit_utils_crash_NativeCrashHandler_installNative(
    env: *mut RawJNIEnv,
    _thiz: jobject,
    crash_log_dir: jstring,
) -> jboolean {
    with_jstring(env, crash_log_dir, |dir| {
        if install_crash_handler(dir) {
            JNI_TRUE
        } else {
            JNI_FALSE
        }
    })
}

/// Uninstall the native crash handler.
///
/// Java signature: `()V`
#[unsafe(no_mangle)]
pub extern "C" fn Java_moe_ouom_wekit_utils_crash_NativeCrashHandler_uninstallNative(
    _env: *mut RawJNIEnv,
    _thiz: jobject,
) {
    uninstall_crash_handler();
}

/// Trigger a deliberate test crash.
///
/// Java signature: `(I)V`
#[unsafe(no_mangle)]
pub extern "C" fn Java_moe_ouom_wekit_utils_crash_NativeCrashHandler_triggerTestCrashNative(
    _env: *mut RawJNIEnv,
    _thiz: jobject,
    crash_type: jint,
) {
    trigger_test_crash(crash_type);
}

/// Convert a Markdown string to HTML.
///
/// Java signature: `(Ljava/lang/String;)Ljava/lang/String;`
#[unsafe(no_mangle)]
pub unsafe extern "C" fn Java_moe_ouom_wekit_hooks_items_chat_MarkdownRendering_convertMarkdownToHtmlNative(
    env: *mut RawJNIEnv,
    _thiz: jobject,
    markdown_string: jstring,
) -> jstring {
    let result = with_jstring(env, markdown_string, |md_text| {
        markdown::to_html_with_options(md_text, &markdown::Options::gfm())
    });

    match result {
        Ok(html) => unsafe {
            let fns = *env;
            let c_str = CString::new(html).unwrap_or_default();
            ((*fns).v1_6.NewStringUTF)(env, c_str.as_ptr())
        },
        Err(_) => std::ptr::null_mut(),
    }
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn Java_moe_ouom_wekit_hooks_api_core_WeNativeHooker_hookNativeFunction(
    _env: *mut RawJNIEnv,
    _thiz: jobject,
    target_ptr: jlong,
    replace_ptr: jlong,
) {
    unsafe {
        let target = target_ptr as *mut c_void;
        let replace = replace_ptr as *mut c_void;
        match dobby_api::hook(target, replace, None) {
            Ok(_) => logi!("hooked native func successfully"),
            Err(err) => loge!("failed to hook native func: {:?}", err),
        }
    }
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn Java_moe_ouom_wekit_hooks_api_core_WeNativeHooker_getSymbolOffset(
    env: *mut RawJNIEnv,
    _thiz: jobject,
    so_path: jstring,
    symbol: jstring,
) -> jlong {
    with_jstring(env, so_path, |path| {
        with_jstring(env, symbol, |sym| {
            match get_symbol_offset_from_file(path, sym) {
                Some(offset) => offset,
                None => {
                    loge!("symbol {} not found in {}", sym, path);
                    0
                }
            }
        })
    })
}

fn get_symbol_offset_from_file(so_path: &str, symbol: &str) -> Option<i64> {
    let (full_path, base_addr) = resolve_full_path_and_base(so_path)?;
    let data = std::fs::read(&full_path).ok()?;
    let elf = Elf::parse(&data).ok()?;

    let sym = elf
        .dynsyms
        .iter()
        .find(|s| elf.dynstrtab.get_at(s.st_name) == Some(symbol))
        .or_else(|| {
            elf.syms
                .iter()
                .find(|s| elf.strtab.get_at(s.st_name) == Some(symbol))
        })?;

    Some(base_addr as i64 + sym.st_value as i64)
}

fn resolve_full_path_and_base(so_name: &str) -> Option<(String, u64)> {
    let name_only = if so_name.starts_with('/') {
        so_name.rsplit('/').next().unwrap_or(so_name)
    } else {
        so_name.rsplit('/').next().unwrap_or(so_name)
    };

    let file = std::fs::File::open("/proc/self/maps").ok()?;

    for line in BufReader::new(file).lines().flatten() {
        if !line.contains(name_only) || !line.contains("r-xp") || !line.contains(" 00000000 ") {
            continue;
        }
        // Parse base address from the start of the range field: "base_addr-end_addr"
        let base_addr = line
            .split('-')
            .next()
            .and_then(|s| u64::from_str_radix(s.trim(), 16).ok())?;

        if let Some(path) = line.split_whitespace().last() {
            if path.starts_with('/') {
                // If caller gave an absolute path, verify it matches
                if so_name.starts_with('/') && path != so_name {
                    continue;
                }
                return Some((path.to_string(), base_addr));
            }
        }
    }
    None
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn Java_moe_ouom_wekit_hooks_api_core_WeNativeHooker_getArtQuickCode(
    env: *mut RawJNIEnv,
    _thiz: jobject,
    method: jobject, // java.lang.reflect.Method
    entry_point_offset: jint,
) -> jlong {
    unsafe {
        // GetMethodID then ToReflectedMethod inverse: use FromReflectedMethod
        // to get the jmethodID, which IS the ArtMethod*
        let art_method_ptr = ((**env).v1_6.FromReflectedMethod)(env, method) as *const u8;

        if art_method_ptr.is_null() {
            loge!("FromReflectedMethod returned null");
            return 0;
        }

        // Read entry_point_from_quick_compiled_code_
        let entry_point_ptr = art_method_ptr.add(entry_point_offset as usize) as *const u64;
        *entry_point_ptr as jlong
    }
}

/// The "real" function we want to hook. Does nothing, returns 42.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn Java_moe_ouom_wekit_hooks_api_core_WeNativeHooker_hookTestTarget(
    _env: *mut RawJNIEnv,
    _thiz: jobject,
) -> jint {
    logi!("hookTestTarget: original called");
    42
}

/// Required JNI library entry point — returns the JNI version we target.
#[unsafe(no_mangle)]
pub extern "C" fn JNI_OnLoad(_vm: *mut JavaVM, _reserved: *mut c_void) -> jint {
    JNI_VERSION_1_6
}

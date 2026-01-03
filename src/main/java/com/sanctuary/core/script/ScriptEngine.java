package com.sanctuary.core.script;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.jse.JsePlatform;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Lua 스크립트를 실행하는 엔진입니다.
 * LuaJ 라이브러리를 사용하며, 샌드박싱과 API 바인딩을 지원합니다.
 */
public class ScriptEngine {

    private final File scriptFolder;
    private final Logger logger;
    private final Globals globals;
    private final LuaBridge bridge;

    // 로드된 스크립트 캐시 (스크립트 이름 -> LuaValue)
    private final Map<String, LuaValue> scriptCache = new HashMap<>();

    public ScriptEngine(File pluginFolder, Logger logger) {
        this.scriptFolder = new File(pluginFolder, "scripts");
        this.logger = logger;
        this.globals = JsePlatform.standardGlobals();
        this.bridge = new LuaBridge(logger);

        if (!scriptFolder.exists()) {
            scriptFolder.mkdirs();
        }

        // 샌드박싱 및 API 등록
        bridge.applySandbox(globals);
        bridge.registerAPI(globals);

        logger.info("[ScriptEngine] 초기화 완료. 스크립트 폴더: " + scriptFolder.getAbsolutePath());
    }

    /**
     * 스크립트 파일을 로드합니다.
     * 캐시된 스크립트가 없으면 파일에서 로드합니다.
     * 
     * @param scriptName 스크립트 파일 이름 (예: "test.lua")
     * @return 로드된 LuaValue 또는 null
     */
    public LuaValue loadScript(String scriptName) {
        // 캐시 확인
        if (scriptCache.containsKey(scriptName)) {
            return scriptCache.get(scriptName);
        }

        File file = new File(scriptFolder, scriptName);
        if (!file.exists()) {
            logger.warning("[ScriptEngine] 스크립트 파일을 찾을 수 없습니다: " + scriptName);
            return null;
        }

        try {
            LuaValue chunk = globals.loadfile(file.getAbsolutePath());
            chunk.call(); // 스크립트 실행하여 함수 정의 로드
            scriptCache.put(scriptName, chunk);
            logger.info("[ScriptEngine] 스크립트 로드됨: " + scriptName);
            return chunk;
        } catch (LuaError e) {
            logger.severe("[ScriptEngine] 스크립트 로드 오류 (" + scriptName + "): " + e.getMessage());
            return null;
        }
    }

    /**
     * 스크립트를 실행합니다.
     * 
     * @param scriptName 스크립트 파일 이름
     */
    public void runScript(String scriptName) {
        loadScript(scriptName);
    }

    /**
     * 스크립트 내의 특정 함수를 실행합니다.
     * 
     * @param scriptName   스크립트 파일 이름
     * @param functionName 호출할 함수 이름
     * @param args         함수에 전달할 인자들
     * @return 함수 반환값 또는 NIL
     */
    public LuaValue executeFunction(String scriptName, String functionName, LuaValue... args) {
        // 스크립트 로드 (필요시)
        loadScript(scriptName);

        try {
            LuaValue func = globals.get(functionName);
            if (func.isnil()) {
                logger.warning("[ScriptEngine] 함수를 찾을 수 없습니다: " + functionName + " in " + scriptName);
                return LuaValue.NIL;
            }

            // 인자 개수에 따라 호출
            switch (args.length) {
                case 0:
                    return func.call();
                case 1:
                    return func.call(args[0]);
                case 2:
                    return func.call(args[0], args[1]);
                case 3:
                    return func.call(args[0], args[1], args[2]);
                default:
                    // 4개 이상의 인자는 invoke 사용
                    Varargs varargs = LuaValue.varargsOf(args);
                    return func.invoke(varargs).arg1();
            }
        } catch (LuaError e) {
            logger.severe("[ScriptEngine] 함수 실행 오류 (" + scriptName + "." + functionName + "): " + e.getMessage());
            return LuaValue.NIL;
        }
    }

    /**
     * 모든 스크립트 캐시를 초기화하고 리로드합니다.
     */
    public void reloadAll() {
        scriptCache.clear();
        logger.info("[ScriptEngine] 스크립트 캐시 초기화됨. 다음 호출 시 재로드됩니다.");
    }

    /**
     * 특정 스크립트를 캐시에서 제거합니다.
     * 
     * @param scriptName 제거할 스크립트 이름
     */
    public void invalidate(String scriptName) {
        scriptCache.remove(scriptName);
    }

    /**
     * Lua Globals 객체를 반환합니다.
     * 
     * @return Globals
     */
    public Globals getGlobals() {
        return globals;
    }

    /**
     * LuaBridge를 반환합니다.
     * 
     * @return LuaBridge
     */
    public LuaBridge getBridge() {
        return bridge;
    }

    /**
     * 스크립트 폴더를 반환합니다.
     * 
     * @return 스크립트 폴더
     */
    public File getScriptFolder() {
        return scriptFolder;
    }
}

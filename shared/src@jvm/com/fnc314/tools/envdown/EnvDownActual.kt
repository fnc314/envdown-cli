package com.fnc314.tools.envdown

public object EnvDownJvm {
    public fun getEnvActual(key: String): String? = System.getenv(key)
}

public actual fun getEnv(key: String): String? = EnvDownJvm.getEnvActual(key)

package com.achtien.codingtemplate

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@Configuration
@ComponentScan
class AppModule {

    @Single
    fun scope() = CoroutineScope(Dispatchers.Default + SupervisorJob() )
}

package eungju.rxcurrencyconverter

import javax.inject.Singleton

import dagger.Component

@Singleton
@Component(modules = arrayOf(ApplicationModule::class))
interface ApplicationComponent {
    fun inject(application: CurrencyApplication)

    fun inject(activity: CurrencyConverterActivity)

    object Builder {
        fun build(app: CurrencyApplication): ApplicationComponent = DaggerApplicationComponent.builder()
                .applicationModule(ApplicationModule(app))
                .build()
    }
}

package eungju.rxcurrencyconverter

import android.app.Application
import android.content.Context

import timber.log.Timber

class CurrencyApplication : Application() {
    private lateinit var component: ApplicationComponent

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        component = ApplicationComponent.Builder.build(this)
        component.inject(this)
    }

    fun component(): ApplicationComponent {
        return component
    }

    companion object {
        operator fun get(context: Context): CurrencyApplication {
            return context.applicationContext as CurrencyApplication
        }
    }
}

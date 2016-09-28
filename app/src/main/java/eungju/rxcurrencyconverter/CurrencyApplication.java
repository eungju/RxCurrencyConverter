package eungju.rxcurrencyconverter;

import android.app.Application;
import android.content.Context;

import timber.log.Timber;

public class CurrencyApplication extends Application {
    private ApplicationComponent component;

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }

        component = ApplicationComponent.Builder.build(this);
        component.inject(this);
    }

    public ApplicationComponent component() {
        return component;
    }

    public static CurrencyApplication get(Context context) {
        return (CurrencyApplication) context.getApplicationContext();
    }
}

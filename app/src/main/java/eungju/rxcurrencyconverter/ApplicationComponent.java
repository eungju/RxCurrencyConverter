package eungju.rxcurrencyconverter;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {ApplicationModule.class})
public interface ApplicationComponent {
    void inject(CurrencyApplication application);

    void inject(CurrencyConverterActivity activity);

    class Builder {
        public static ApplicationComponent build(CurrencyApplication app) {
            return DaggerApplicationComponent.builder()
                    .applicationModule(new ApplicationModule(app))
                    .build();
        }
    }
}

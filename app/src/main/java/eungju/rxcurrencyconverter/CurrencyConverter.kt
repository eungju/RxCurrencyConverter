package eungju.rxcurrencyconverter

import com.jakewharton.rxrelay.BehaviorRelay
import com.jakewharton.rxrelay.PublishRelay
import rx.Observable
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import javax.inject.Inject

class CurrencyConverter @Inject constructor(fixer: Fixer) {
    val refresh: PublishRelay<Void> = PublishRelay.create()
    val refreshing: BehaviorRelay<Boolean> = BehaviorRelay.create(false)
    val fromCurrencySet: BehaviorRelay<Currency> = BehaviorRelay.create(Currency.getInstance("USD"))
    val fromCurrencyUpdate: PublishRelay<Currency> = PublishRelay.create()
    val fromAmountSet: BehaviorRelay<BigDecimal> = BehaviorRelay.create(BigDecimal.ONE)
    val fromAmountUpdate: PublishRelay<BigDecimal> = PublishRelay.create()
    val toCurrencySet: BehaviorRelay<Currency> = BehaviorRelay.create(Currency.getInstance("KRW"))
    val toCurrencyUpdate: PublishRelay<Currency> = PublishRelay.create()
    val toAmountSet: BehaviorRelay<BigDecimal> = BehaviorRelay.create()
    val toAmountUpdate: PublishRelay<BigDecimal> = PublishRelay.create()

    val fromCurrency: Observable<Currency> = Observable.merge(fromCurrencySet, fromCurrencyUpdate)
    val toCurrency: Observable<Currency> = Observable.merge(toCurrencySet, toCurrencyUpdate)
    private val currencyRates: Observable<CurrencyRates> = fromCurrency.distinctUntilChanged()
            .mergeWith(refresh.withLatestFrom(fromCurrency, { refresh, fromCurrency -> fromCurrency }))
            .doOnNext { refreshing.call(true) }
            .concatMap {
                fixer.latest(it.currencyCode)
                        .doOnError { refreshing.call(false) }
                        .onErrorResumeNext(Observable.empty())
            }
            .doOnNext { refreshing.call(false) }
            .share()
    val date: Observable<String> = currencyRates.map { it.date }
    val currencies: Observable<List<Currency>> = currencyRates
            .map { (it.rates.keys + it.base).sorted().map { Currency.getInstance(it) } }
    val fromAmount: Observable<BigDecimal> = Observable.merge(fromAmountSet, fromAmountUpdate)
            .mergeWith(Observable.merge(toAmountSet, toAmountUpdate).withLatestFrom(fromCurrency, toCurrency, currencyRates, { amount, from, to, rates ->
                if (rates.base == to.currencyCode) {
                    amount
                } else {
                    amount.divide(BigDecimal(rates.rates[to.currencyCode]!!), from.defaultFractionDigits, RoundingMode.FLOOR)
                }
            }))
    val toAmount: Observable<BigDecimal> = Observable.merge(toAmountSet, toAmountUpdate)
            .mergeWith(Observable.combineLatest(Observable.merge(fromAmountSet, fromAmountUpdate), fromCurrency, toCurrency, currencyRates, { amount, from, to, rates ->
                if (rates.base == to.currencyCode) {
                    amount
                } else {
                    amount.multiply(BigDecimal(rates.rates[to.currencyCode]!!)).setScale(to.defaultFractionDigits, RoundingMode.FLOOR)
                }
            }))
}
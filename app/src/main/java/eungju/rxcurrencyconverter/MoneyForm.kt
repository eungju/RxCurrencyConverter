package eungju.rxcurrencyconverter

import com.jakewharton.rxrelay.BehaviorRelay
import com.jakewharton.rxrelay.PublishRelay
import rx.Observable
import java.math.BigDecimal
import java.util.*

class MoneyForm {
    val currencies: BehaviorRelay<List<Currency>> = BehaviorRelay.create()
    val currencySet: BehaviorRelay<Currency> = BehaviorRelay.create()
    val currencySelect: PublishRelay<Int> = PublishRelay.create()
    val amountSet: BehaviorRelay<BigDecimal> = BehaviorRelay.create()
    val keyPress: PublishRelay<String> = PublishRelay.create()

    val currency: Observable<Currency> = currencySet
            .mergeWith(Observable.combineLatest(currencySelect, currencies, { select, currencies -> currencies[select] }))
            .cacheWithInitialCapacity(1)
    val currencySetIndex: Observable<Int> = Observable.combineLatest(currencySet, currencies, { currency, currencies ->
                currencies.indexOf(currency)
            })
            .cacheWithInitialCapacity(1)
    val amount: Observable<BigDecimal> = amountSet.map { amount -> { current: BigDecimal -> amount } }
            .mergeWith(keyPress.map { keyPress ->
                when (keyPress) {
                    "delete" -> { current: BigDecimal -> current.divideToIntegralValue(BigDecimal.TEN) }
                    "clear" -> { current: BigDecimal -> BigDecimal.ZERO }
                    else -> { current: BigDecimal ->
                        current
                                .divideToIntegralValue(BigDecimal.ONE)
                                .multiply(BigDecimal.TEN)
                                .plus(BigDecimal(Integer.parseInt(keyPress).toDouble()))
                    }
                }
            })
            .scan(BigDecimal.ZERO, { current, f -> f(current) })
            .skip(1)
            .cacheWithInitialCapacity(1)
    val currencyUpdate: Observable<Currency> = currencySelect
            .withLatestFrom(currency, { currencySelect, currency -> currency })
    val amountUpdate: Observable<BigDecimal> = keyPress
            .withLatestFrom(amount, { keyPress, amount -> amount })
}
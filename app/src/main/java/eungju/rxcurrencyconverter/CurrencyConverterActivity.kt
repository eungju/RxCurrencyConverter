package eungju.rxcurrencyconverter

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.jakewharton.rxbinding2.support.v4.widget.RxSwipeRefreshLayout
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_currency_converter.*
import javax.inject.Inject

class CurrencyConverterActivity : AppCompatActivity() {
    @Inject lateinit var presenter: CurrencyConverter
    private lateinit var subscriptions: CompositeDisposable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_currency_converter)

        CurrencyApplication.get(this).component().inject(this)

        subscriptions = CompositeDisposable(
                //output
                presenter.refreshing
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(RxSwipeRefreshLayout.refreshing(refresh)),
                presenter.currencies
                        .subscribe {
                            from.setCurrencies(it)
                            to.setCurrencies(it)
                        },
                presenter.fromCurrency.subscribe { from.setCurrency(it) },
                presenter.fromAmount.subscribe { from.setAmount(it) },
                presenter.toCurrency.subscribe { to.setCurrency(it) },
                presenter.toAmount.subscribe { to.setAmount(it) },
                //input
                RxSwipeRefreshLayout.refreshes(refresh).map { Unit }.subscribe(presenter.refresh),
                from.currencyUpdate().subscribe(presenter.fromCurrencyUpdate),
                from.amountUpdate().subscribe(presenter.fromAmountUpdate),
                to.currencyUpdate().subscribe(presenter.toCurrencyUpdate),
                to.amountUpdate().subscribe(presenter.toAmountUpdate)

        )
    }

    override fun onDestroy() {
        subscriptions.dispose()
        super.onDestroy()
    }
}

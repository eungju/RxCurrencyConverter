package eungju.rxcurrencyconverter

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.jakewharton.rxbinding.support.v4.widget.RxSwipeRefreshLayout
import com.jakewharton.rxbinding.widget.RxTextView
import kotlinx.android.synthetic.main.activity_currency_converter.*
import rx.android.schedulers.AndroidSchedulers
import rx.subscriptions.CompositeSubscription
import javax.inject.Inject

class CurrencyConverterActivity : AppCompatActivity() {
    @Inject lateinit var presenter: CurrencyConverter
    private lateinit var subscriptions: CompositeSubscription

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_currency_converter)

        CurrencyApplication.get(this).component().inject(this)

        subscriptions = CompositeSubscription()
        //output
        subscriptions.add(presenter.refreshing
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(RxSwipeRefreshLayout.refreshing(refresh)))
        subscriptions.add(presenter.date
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(RxTextView.text(this.date)))
        subscriptions.add(presenter.currencies
                .subscribe {
                    from.setCurrencies(it)
                    to.setCurrencies(it)
                })
        subscriptions.add(presenter.fromCurrency
                .subscribe { from.setCurrency(it) })
        subscriptions.add(presenter.fromAmount
                .subscribe { from.setAmount(it) })
        subscriptions.add(presenter.toCurrency
                .subscribe { to.setCurrency(it) })
        subscriptions.add(presenter.toAmount
                .subscribe { to.setAmount(it) })
        //input
        subscriptions.add(RxSwipeRefreshLayout.refreshes(refresh).subscribe(presenter.refresh))
        subscriptions.add(from.currencyUpdate().subscribe(presenter.fromCurrencyUpdate))
        subscriptions.add(from.amountUpdate().subscribe(presenter.fromAmountUpdate))
        subscriptions.add(to.currencyUpdate().subscribe(presenter.toCurrencyUpdate))
        subscriptions.add(to.amountUpdate().subscribe(presenter.toAmountUpdate))
    }

    override fun onDestroy() {
        subscriptions.unsubscribe()
        super.onDestroy()
    }
}

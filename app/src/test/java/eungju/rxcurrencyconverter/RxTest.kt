package eungju.rxcurrencyconverter

import org.junit.Test
import rx.Observable
import rx.observers.TestSubscriber

class RxTest {
    @Test
    fun scan() {
        val subscriber = TestSubscriber.create<Int>()
        Observable.range(0, 3).scan(0, { current, e -> current + e }).subscribe(subscriber)
        subscriber.assertValues(0, 0, 1, 3)
    }
}
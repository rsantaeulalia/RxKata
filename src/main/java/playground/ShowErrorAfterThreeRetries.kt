package playground

import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import utils.mainScheduler
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

/**
 * Display a retry message for every retry and then after 3 retries display an error message
 *
 * https://stackoverflow.com/questions/48874623/rxjava-retry-reset-retry-counter-on-successful-resume
 */

class ShowErrorAfterThreeRetries {

    val single =
        Single.error<Throwable>(IllegalStateException())
            .retryWhen { errors ->
                val counter = AtomicInteger(0)
                /*
                This is wrong because the `takeWhile` will complete and then the error will display: "Displaying error!! java.util.NoSuchElementException"
                To fix this, we need to use a `flatMap` instead (please check the test for this) or catch the completion event with a `repeatWhen`
                 */
                errors.takeWhile { counter.getAndIncrement() != 3 /*&& it is IOException*/ }
//                            .repeatWhen { it.flatMapSingle { Single.error<Throwable>(IllegalStateException()) } }
                    .observeOn(mainScheduler)
                    .doOnNext { println("Retrying... $counter ${Thread.currentThread().name}") }
                    .observeOn(Schedulers.computation())
            }
}

fun main() {
    val countDownLatch = CountDownLatch(1)
    ShowErrorAfterThreeRetries().single
        .subscribeBy(
            onSuccess = { println("Success!") },
            onError = {
                println("Displaying error: $it")
                countDownLatch.countDown()
            })

    countDownLatch.await()
}
package playground.twitter

import BehaviorSubjectWithDefault
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.combineLatest
import io.reactivex.rxjava3.schedulers.Schedulers
import java.time.Instant

/**
Design a simplified version of Twitter where users can post tweets,
follow/unfollow another user and is able to see the 10 most recent tweets in the user's news feed.

Your design should support the following methods:

postTweet(userId, tweetId): Compose a new tweet. get News Feed(user Id): Retrieve the 10 most recent tweet ids in the user's news feed.
Each item in the news feed must be posted by users who the user followed or by the user herself.
Tweets must be ordered from most recent to least recent.

follow(followerId, followed): Follower follows a followee.

unfollow(followerId, followeeId): Follower unfollow a followee.
 */

typealias UserId = Int
typealias TweetId = Int
typealias FollowerId = Int
typealias FolloweeId = Int

interface ITwitterExercise {
    fun postTweet(userId: UserId, tweetId: TweetId)
    fun getNewsFeed(userId: UserId): Observable<List<TweetId>>
    fun getFollowees(followerId: FollowerId): Observable<Set<FolloweeId>>
    fun follow(followeeId: FolloweeId, followerId: FollowerId)
    fun unfollow(followerId: FollowerId, followeeId: FolloweeId)
}

class TwitterExercise : ITwitterExercise {
    private data class TweetDetails(val time: Instant, val id: TweetId)

    private val tweets: MutableMap<UserId, BehaviorSubjectWithDefault<List<TweetDetails>>> = mutableMapOf()
    private val followees: MutableMap<UserId, BehaviorSubjectWithDefault<Set<FolloweeId>>> = mutableMapOf()

    private fun tweets(userId: UserId) =
        synchronized(tweets) {
            tweets.getOrPut(userId) { BehaviorSubjectWithDefault.create(emptyList()) }
        }

    private fun followees(followerId: UserId) =
        synchronized(followees) {
            followees.getOrPut(followerId) { BehaviorSubjectWithDefault.create(emptySet()) }
        }

    override fun postTweet(userId: UserId, tweetId: TweetId) {
        val userTweets = tweets(userId)
        userTweets.onNext(userTweets.value + TweetDetails(Instant.now(), tweetId))
    }

    override fun getNewsFeed(userId: UserId): Observable<List<TweetId>> =
        followees(userId)
            .observeOn(Schedulers.computation())
            .switchMap { userFollowees ->
                (userFollowees + userId).takeTenSortedTweetIds()
            }
            .distinctUntilChanged()

    override fun getFollowees(followerId: FollowerId) = followees(followerId)
        .switchMap { userFollowees ->
            userFollowees.map { followeeId ->
                followees(followeeId)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.computation())
            }.combineLatest { it.flatten().toSet() }
                .map { it + userFollowees }
        }.distinctUntilChanged()

    private fun Iterable<UserId>.takeTenSortedTweetIds() =
        map { tweets(it).observeOn(Schedulers.computation()) }
            .combineLatest { it.flatten() }
            .map { tweets ->
                tweets.sortedByDescending { it.time }
                    .take(10)
                    .map { it.id }
            }

    override fun follow(followeeId: FolloweeId, followerId: FollowerId) {
        val userFollowees = followees(followerId)
        userFollowees.onNext(userFollowees.value + followeeId)
    }

    override fun unfollow(followerId: UserId, followeeId: UserId) {
        val userFollowees = followees(followerId)
        userFollowees.onNext(userFollowees.value - followeeId)
    }
}
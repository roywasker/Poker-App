package com.example.poker.data

import com.example.poker.data.repository.GameRepositoryImplTest
import com.example.poker.data.repository.PlayerRepositoryImplTest
import com.example.poker.data.repository.UserRepositoryImplTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Test suite that runs all Repository implementation tests together.
 *
 * This suite includes tests for:
 * - UserRepositoryImpl: User management (add, list, update balance)
 * - PlayerRepositoryImpl: Player listing with balances
 * - GameRepositoryImpl: Game history, statistics, and balance updates
 *
 * To run all repository tests:
 * ./gradlew :app:testDebugUnitTest --tests com.example.poker.data.repository.AllRepositoryTestSuite
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    UserRepositoryImplTest::class,
    PlayerRepositoryImplTest::class,
    GameRepositoryImplTest::class
)
class AllRepositoryTestSuite
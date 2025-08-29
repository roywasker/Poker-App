package com.example.poker.data

import com.example.poker.data.database.PlayerSessionRepositoryMockTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Test suite that runs all database-related tests together.
 *
 * This suite includes tests for:
 * - PlayerSessionDao: Room database DAO operations
 * - PlayerSessionRepository: Repository layer with JSON conversion
 *
 * Note: PlayerSessionDaoTest requires Android instrumentation.
 * For unit tests only, run PlayerSessionRepositoryTest separately.
 *
 * To run all database tests:
 * ./gradlew :app:testDebugUnitTest --tests com.example.poker.data.database.AllDatabaseTestSuite
 *
 * To run instrumented DAO tests:
 * ./gradlew :app:connectedAndroidTest --tests com.example.poker.data.database.PlayerSessionDaoTest
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    PlayerSessionRepositoryMockTest::class
    // PlayerSessionDaoTest requires instrumentation and should be run separately
)
class AllDatabaseTestSuite
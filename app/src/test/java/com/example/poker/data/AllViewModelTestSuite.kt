package com.example.poker.data

import com.example.poker.data.viewmodel.AddUserViewModelTest
import com.example.poker.data.viewmodel.HistoryByDayViewModelNetworkTest
import com.example.poker.data.viewmodel.HistoryByDayViewModelTest
import com.example.poker.data.viewmodel.PlayerBalanceViewModelNetworkTest
import com.example.poker.data.viewmodel.PlayerBalanceViewModelTest
import com.example.poker.data.viewmodel.PlayerStatisticsViewModelNetworkTest
import com.example.poker.data.viewmodel.PlayerStatisticsViewModelTest
import com.example.poker.data.viewmodel.StartGameViewModelNetworkTest
import com.example.poker.data.viewmodel.StartGameViewModelTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Test suite that runs all ViewModel tests together.
 * 
 * This suite includes tests for:
 * - AddUserViewModel: User management and balance operations
 * - StartGameViewModel: Game session management and transfers
 * - PlayerStatisticsViewModel: Player statistics and performance metrics
 * - HistoryByDayViewModel: Historical game data by date
 * - PlayerBalanceViewModel: Player balance tracking
 * 
 * To run all ViewModel tests:
 * ./gradlew :app:testDebugUnitTest --tests com.example.poker.data.AllViewModelTestSuite
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    AddUserViewModelTest::class,
    StartGameViewModelTest::class,
    PlayerStatisticsViewModelTest::class,
    HistoryByDayViewModelTest::class,
    PlayerBalanceViewModelTest::class,
    StartGameViewModelNetworkTest::class,
    PlayerBalanceViewModelNetworkTest::class,
    HistoryByDayViewModelNetworkTest::class,
    PlayerStatisticsViewModelNetworkTest::class
)
class AllViewModelTestSuite
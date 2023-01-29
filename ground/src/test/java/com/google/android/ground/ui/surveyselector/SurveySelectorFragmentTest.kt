/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.ground.ui.surveyselector

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import com.google.android.ground.*
import com.google.android.ground.model.Survey
import com.google.android.ground.repository.SurveyRepository
import com.google.android.ground.ui.common.Navigator
import com.google.android.ground.ui.home.HomeScreenFragmentDirections
import com.google.common.collect.ImmutableList
import com.google.common.truth.Truth
import com.sharedtest.FakeData
import com.sharedtest.system.auth.FakeAuthenticationManager
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import io.reactivex.*
import java8.util.Optional
import javax.inject.Inject
import org.hamcrest.Matchers.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class SurveySelectorFragmentTest : BaseHiltTest() {

  @BindValue @Mock lateinit var navigator: Navigator
  @BindValue @Mock lateinit var surveyRepository: SurveyRepository
  @Inject lateinit var fakeAuthenticationManager: FakeAuthenticationManager

  private lateinit var fragment: SurveySelectorFragment
  private lateinit var activeSurveyFlowableEmitter: FlowableEmitter<Optional<Survey>>

  @Before
  override fun setUp() {
    super.setUp()
    fakeAuthenticationManager.setUser(TEST_USER)

    whenever(surveyRepository.activeSurvey)
      .thenReturn(
        Flowable.create({ activeSurveyFlowableEmitter = it }, BackpressureStrategy.LATEST)
      )
  }

  @Test
  fun created_surveysAvailable_whenNoSurveySynced() {
    setAllSurveys(listOf(TEST_SURVEY_1, TEST_SURVEY_2))
    setOfflineSurveys(ImmutableList.of())
    setUpFragment()

    // Assert that 2 surveys are displayed
    onView(withId(R.id.recycler_view)).check(matches(allOf(isDisplayed(), hasChildCount(2))))

    var viewHolder = getViewHolder(0)
    Truth.assertThat(viewHolder.binding.title.text).isEqualTo(TEST_SURVEY_1.title)
    Truth.assertThat(viewHolder.binding.description.text).isEqualTo(TEST_SURVEY_1.description)
    Truth.assertThat(viewHolder.binding.offlineIcon.visibility).isEqualTo(View.GONE)

    viewHolder = getViewHolder(1)
    Truth.assertThat(viewHolder.binding.title.text).isEqualTo(TEST_SURVEY_2.title)
    Truth.assertThat(viewHolder.binding.description.text).isEqualTo(TEST_SURVEY_2.description)
    Truth.assertThat(viewHolder.binding.offlineIcon.visibility).isEqualTo(View.GONE)
  }

  @Test
  fun created_surveysAvailable_whenOneSurveySynced() {
    setAllSurveys(listOf(TEST_SURVEY_1, TEST_SURVEY_2))
    setOfflineSurveys(ImmutableList.of(TEST_SURVEY_2))
    setUpFragment()

    // Assert that 2 surveys are displayed
    onView(withId(R.id.recycler_view)).check(matches(allOf(isDisplayed(), hasChildCount(2))))

    var viewHolder = getViewHolder(0)
    Truth.assertThat(viewHolder.binding.title.text).isEqualTo(TEST_SURVEY_1.title)
    Truth.assertThat(viewHolder.binding.description.text).isEqualTo(TEST_SURVEY_1.description)
    Truth.assertThat(viewHolder.binding.offlineIcon.visibility).isEqualTo(View.GONE)

    viewHolder = getViewHolder(1)
    Truth.assertThat(viewHolder.binding.title.text).isEqualTo(TEST_SURVEY_2.title)
    Truth.assertThat(viewHolder.binding.description.text).isEqualTo(TEST_SURVEY_2.description)
    Truth.assertThat(viewHolder.binding.offlineIcon.visibility).isEqualTo(View.VISIBLE)
  }

  @Test
  fun click_activatesSurvey() {
    setAllSurveys(listOf(TEST_SURVEY_1, TEST_SURVEY_2))
    setOfflineSurveys(ImmutableList.of())
    setUpFragment()

    // Click second item
    onView(withId(R.id.recycler_view))
      .perform(RecyclerViewActions.actionOnItemAtPosition<SurveyListAdapter.ViewHolder>(1, click()))

    // Assert that survey was activated
    verify(surveyRepository).activateSurvey(eq(TEST_SURVEY_2.id))
  }

  @Test
  fun surveyActivated_whenNothingClicked() {
    setAllSurveys(listOf(TEST_SURVEY_1, TEST_SURVEY_2))
    setOfflineSurveys(ImmutableList.of(TEST_SURVEY_2))
    setUpFragment()

    // Activate survey
    activeSurveyFlowableEmitter.onNext(Optional.of(TEST_SURVEY_2))

    // Assert that no navigation was requested
    verifyNoInteractions(navigator)
  }

  @Test
  fun surveyActivated_whenSurveyClicked() {
    setAllSurveys(listOf(TEST_SURVEY_1, TEST_SURVEY_2))
    setOfflineSurveys(ImmutableList.of(TEST_SURVEY_2))
    setUpFragment()

    // Click second item
    onView(withId(R.id.recycler_view))
      .perform(RecyclerViewActions.actionOnItemAtPosition<SurveyListAdapter.ViewHolder>(1, click()))

    // Activate survey
    activeSurveyFlowableEmitter.onNext(Optional.of(TEST_SURVEY_2))

    // Assert that navigation to home screen was requested
    verify(navigator).navigate(HomeScreenFragmentDirections.showHomeScreen())
  }

  private fun setUpFragment() {
    launchFragmentInHiltContainer<SurveySelectorFragment> {
      fragment = this as SurveySelectorFragment
    }
  }

  private fun setAllSurveys(surveys: List<Survey>) {
    whenever(surveyRepository.getSurveySummaries(FakeData.USER)).thenReturn(Single.just(surveys))
  }

  private fun setOfflineSurveys(surveys: ImmutableList<Survey>) {
    whenever(surveyRepository.offlineSurveys).thenReturn(Single.just(surveys))
  }

  private fun getViewHolder(index: Int): SurveyListAdapter.ViewHolder {
    val recyclerView = fragment.view?.findViewById<RecyclerView>(R.id.recycler_view)
    val viewHolder = recyclerView?.findViewHolderForAdapterPosition(index)
    return viewHolder as SurveyListAdapter.ViewHolder
  }

  companion object {
    private val TEST_USER = FakeData.USER
    private val TEST_SURVEY_1 =
      FakeData.SURVEY.copy(id = "1", title = "survey 1", description = "description 1")
    private val TEST_SURVEY_2 =
      FakeData.SURVEY.copy(id = "2", title = "survey 2", description = "description 2")
  }
}

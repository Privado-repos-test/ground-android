/*
 * Copyright 2018 Google LLC
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

import androidx.lifecycle.viewModelScope
import com.google.android.ground.coroutines.ApplicationScope
import com.google.android.ground.coroutines.IoDispatcher
import com.google.android.ground.domain.usecases.survey.ActivateSurveyUseCase
import com.google.android.ground.model.SurveyListItem
import com.google.android.ground.repository.SurveyRepository
import com.google.android.ground.repository.UserRepository
import com.google.android.ground.system.auth.AuthenticationManager
import com.google.android.ground.ui.common.AbstractViewModel
import com.google.android.ground.ui.common.Navigator
import com.google.android.ground.ui.home.HomeScreenFragmentDirections
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

/** Represents view state and behaviors of the survey selector dialog. */
class SurveySelectorViewModel
@Inject
internal constructor(
  private val surveyRepository: SurveyRepository,
  private val authManager: AuthenticationManager,
  private val navigator: Navigator,
  private val activateSurveyUseCase: ActivateSurveyUseCase,
  @ApplicationScope private val externalScope: CoroutineScope,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
  private val userRepository: UserRepository
) : AbstractViewModel() {

  val surveyListState: MutableStateFlow<State?> = MutableStateFlow(null)
  val surveySummaries: Flow<List<SurveyListItem>>

  init {
    surveySummaries =
      getSurveyList().onEach {
        if (it.isEmpty()) {
          setNotFound()
        } else {
          setLoaded()
        }
      }
  }

  /** Returns a flow of [SurveyListItem] to be displayed to the user. */
  private fun getSurveyList(): Flow<List<SurveyListItem>> =
    surveyRepository
      .getSurveyList(authManager.currentUser)
      .onStart { setLoading() }
      .map { surveys -> surveys.sortedBy { it.title }.sortedByDescending { it.availableOffline } }

  /** Triggers the specified survey to be loaded and activated. */
  fun activateSurvey(surveyId: String) =
    viewModelScope.launch {
      // TODO(#1497): Handle exceptions thrown by activateSurvey().
      setLoading()
      activateSurveyUseCase(surveyId)
      setLoaded()
      navigateToHomeScreen()
    }

  private fun navigateToHomeScreen() {
    navigator.navigate(HomeScreenFragmentDirections.showHomeScreen())
  }

  fun deleteSurvey(surveyId: String) {
    externalScope.launch(ioDispatcher) { surveyRepository.removeOfflineSurvey(surveyId) }
  }

  fun signOut() {
    userRepository.signOut()
  }

  private fun setNotFound() {
    surveyListState.value = State.NOT_FOUND
  }

  private fun setLoading() {
    surveyListState.value = State.LOADING
  }

  private fun setLoaded() {
    surveyListState.value = State.LOADED
  }

  enum class State {
    NOT_FOUND,
    LOADING,
    LOADED,
  }
}

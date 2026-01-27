#if (${PACKAGE_NAME} && ${PACKAGE_NAME} != "")package ${PACKAGE_NAME}#end

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class ${NAME}ViewModel @Inject constructor(
    private val repository: ${NAME}Repository
) : ViewModel() {

    private val _uiState = MutableStateFlow(${NAME}UiState())
    val uiState: StateFlow<${NAME}UiState> = _uiState.asStateFlow()

    fun onEvent(event: ${NAME}Event) {
        when (event) {
            ${NAME}Event.OnInit -> {
            }
        }
    }
}

sealed interface ${NAME}Event {

    data object OnInit : ${NAME}Event

}
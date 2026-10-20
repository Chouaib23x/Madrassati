package dz.madrassati.core.session

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class ActiveStudent @Inject constructor() {

    private val _studentId = MutableStateFlow<Long?>(null)
    val studentId: StateFlow<Long?> = _studentId.asStateFlow()

    private val _name = MutableStateFlow<String?>(null)
    val name: StateFlow<String?> = _name.asStateFlow()

    fun activate(studentId: Long, name: String) {
        _studentId.value = studentId
        _name.value = name
    }

    fun clear() {
        _studentId.value = null
        _name.value = null
    }
}
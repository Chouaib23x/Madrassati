package dz.madrassati.domain.usecase

import dz.madrassati.domain.repository.ClassRepository
import dz.madrassati.domain.repository.ContentRepository
import javax.inject.Inject

class SeedContentUseCase @Inject constructor(
    private val contentRepository: ContentRepository
) {
    suspend operator fun invoke() {
        contentRepository.seed()
    }
}

class AddStudentUseCase @Inject constructor(
    private val classRepository: ClassRepository
) {
    suspend operator fun invoke(name: String, avatar: String, colorHex: Long): AddStudentResult {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return AddStudentResult.EmptyName
        if (classRepository.count() >= MAX_STUDENTS) return AddStudentResult.Full
        classRepository.add(trimmed, avatar, colorHex)
        return AddStudentResult.Success
    }

    companion object {
        const val MAX_STUDENTS = 20
    }
}

sealed interface AddStudentResult {
    data object Success : AddStudentResult
    data object EmptyName : AddStudentResult
    data object Full : AddStudentResult
}

package org.tensorflow.lite.examples.detection.clean.domain.repositories

import android.graphics.Bitmap
import kotlinx.coroutines.flow.StateFlow
import org.tensorflow.lite.examples.detection.clean.data.models.MemberModel

interface InMemoryMembersRepository {
    suspend fun setMember(memberName: String, faceBmp: Bitmap?)
    val getMember: StateFlow<MemberModel?>
}
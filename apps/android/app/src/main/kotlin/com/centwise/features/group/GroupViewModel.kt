package com.centwise.features.group

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow

class GroupViewModel(
    private val repository: GroupRepository = GroupRepository.shared
) : ViewModel() {

    val currentGroup: StateFlow<SharedGroup?> = repository.currentGroup
    val allGroups: StateFlow<List<SharedGroup>> = repository.allGroups

    fun selectGroup(groupId: String) {
        repository.selectGroup(groupId)
    }

    fun addExpenseEntry(spenderId: String, items: List<ExpenseItem>, notes: String = "") {
        repository.addExpenseEntry(spenderId, items, notes)
    }

    fun updateExpenseEntry(entryId: String, spenderId: String, items: List<ExpenseItem>, notes: String = "") {
        repository.updateExpenseEntry(entryId, spenderId, items, notes)
    }

    fun deleteExpenseEntry(entryId: String) {
        repository.deleteExpenseEntry(entryId)
    }

    fun deleteExpenseEntriesForDate(date: String) {
        repository.deleteExpenseEntriesForDate(date)
    }

    fun addMemberDeposit(memberId: String, amount: Double, paymentMethod: String = "Cash", note: String = "") {
        repository.addMemberDeposit(memberId, amount, paymentMethod, note)
    }

    fun createGroup(name: String, type: GroupType, description: String, monthlyTarget: Double, creatorName: String) {
        repository.createGroup(name, type, description, monthlyTarget, creatorName)
    }

    fun joinGroup(joinCode: String, newMemberName: String) {
        repository.joinGroup(joinCode, newMemberName)
    }

    fun resetToMockData() {
        repository.resetToMockData()
    }

    fun clearGroupForTesting() {
        repository.clearGroupForTesting()
    }
}

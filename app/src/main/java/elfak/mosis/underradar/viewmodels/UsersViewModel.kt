package elfak.mosis.underradar.viewmodels

import android.content.ContentValues
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import elfak.mosis.underradar.data.User

class UsersViewModel : ViewModel() {
    private val _users= MutableLiveData<List<User>>(emptyList())
    private val database= Firebase.database.reference

    var users
        get()=_users.value
        set(value){ _users.value=value}

    fun getUsers(onDataLoaded: () -> Unit)
    {
        database.child("Users").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.exists())
                {
                    val userList= mutableListOf<User>()
                    for(user in snapshot.children)
                    {
                        val u=user.getValue(User::class.java)
                        userList.add(u!!)
                    }
                    if(users==null)
                    {
                        users=userList.sortedByDescending { it.points }
                        onDataLoaded()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(ContentValues.TAG, "Failed to read value.", error.toException());
            }
        })
    }
}
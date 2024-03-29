package elfak.mosis.underradar.viewmodels

import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Context
import android.net.Uri
import android.text.BoringLayout
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import elfak.mosis.underradar.data.Comment
import elfak.mosis.underradar.data.Device
import elfak.mosis.underradar.data.User
import java.lang.Math.atan2
import java.lang.Math.cos
import java.lang.Math.sin
import java.lang.Math.sqrt
import java.net.URI
import java.util.UUID

class DeviceViewModel : ViewModel() {

    private val database= Firebase.database.reference
    private val storageRef = FirebaseStorage.getInstance().reference

    private val _device=MutableLiveData<Device?>(null)
    private val _devices=MutableLiveData<List<Device>>(emptyList())

    var all: Boolean=true
    var camera: Boolean=false
    var radar:Boolean=false
    var device
        get() = _device.value
        set(value) { _device.value=value}

    val devices: LiveData<List<Device>> get() = _devices

    fun addDevice(device: Device, user: User, imgURI: Uri?=null)
    {
        if(imgURI!=null)
        {
            val uuid= UUID.randomUUID().toString()+".jpg"
            storageRef.child("Device photo").child(uuid).putFile(imgURI).addOnSuccessListener{
                storageRef.child("Device photo").child(uuid).downloadUrl.addOnSuccessListener { uri->
                    device.imageURL=uri.toString()
                    database.child("Devices").child(device.id).setValue(device)
                }
            }
        }
        else
        {
            database.child("Devices").child(device.id).setValue(device)
        }
        database.child("Users").child(device.ownerId).child("points").setValue(user.points+10)
    }
    private fun getDistance(currentLat: Double, currentLon: Double, deviceLat: Double, deviceLon: Double): Double {
        val earthRadius = 6371000.0 // Earth's radius in meters

        val currentLatRad = Math.toRadians(currentLat)
        val deviceLatRad = Math.toRadians(deviceLat)
        val deltaLat = Math.toRadians(deviceLat - currentLat)
        val deltaLon = Math.toRadians(deviceLon - currentLon)

        val a = sin(deltaLat / 2) * sin(deltaLat / 2) +
                cos(currentLatRad) * cos(deviceLatRad) *
                sin(deltaLon / 2) * sin(deltaLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }

    fun filterLocations(all:Boolean?, camera:Boolean?, radar:Boolean?, loc: LatLng, rad: Int=10000)
    {
        this.all= all!!
        this.camera=camera!!
        this.radar=radar!!
        getDevices(location=loc, radius=rad, onDataLoaded = {})
    }
    fun getDevices(location: LatLng, radius: Int=10000, onDataLoaded: () -> Unit)
    {
        database.child("Devices").addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.exists())
                {
                    val deviceList= mutableListOf<Device>()
                    for(dev in snapshot.children){
                        val d=dev.getValue(Device::class.java)
                        d?.let{
                            val distance=getDistance(location.latitude, location.longitude, d.latitude, d.longitude)
                            if(distance<=radius)
                            {
                                if(all)
                                {
                                    deviceList.add(d)
                                }
                                else if(camera)
                                {
                                    if(d?.type=="Camera")
                                    {
                                        deviceList.add(d)
                                    }
                                }
                                else if (radar)
                                {
                                    if(d?.type=="Radar")
                                    {
                                        deviceList.add(d)
                                    }
                                }
                            }
                            }
                        }
                    _devices.value=deviceList
                    onDataLoaded()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(ContentValues.TAG, "Failed to read value.", error.toException());
            }
        })
    }

    fun like(user:User)
    {
        device!!.like=device!!.like+1
        database.child("Devices").child(device!!.id).child("like").setValue(device!!.like)
        database.child("Users").child(user.id).child("points").setValue(user.points+10)
    }

    fun dislike(user:User)
    {
        device!!.dislike=device!!.dislike+1
        database.child("Devices").child(device!!.id).child("dislike").setValue(device!!.dislike)
        database.child("Users").child(user.id).child("points").setValue(user.points+10)
    }
}
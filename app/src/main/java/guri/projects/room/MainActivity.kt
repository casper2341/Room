package guri.projects.room

import android.app.AlertDialog
import android.app.Dialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import guri.projects.room.databinding.ActivityMainBinding
import guri.projects.room.databinding.DialogUpdateBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private var binding : ActivityMainBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)


        val employeeDao = (application as EmployeeApp).db.employeeDao()


        binding?.btnAdd?.setOnClickListener {
            addRecord(employeeDao)
        }


        lifecycleScope.launch{
            employeeDao.fetchAllEmployees().collect{

                val list = ArrayList(it)
                setUpRecyclerView(list, employeeDao)
            }
        }
    }

    //adding record to the database

    fun addRecord(employeeDao: EmployeeDao)
    {
        val name = binding?.etName?.text.toString()
        val email = binding?.etEmailId?.text.toString()


        if(name.isNotEmpty() && email.isNotEmpty())
        {

            //use of coroutines to make it to other thread

            lifecycleScope.launch {

                //data insertion in database using insert function in dao
                employeeDao.insert(EmployeeEntity(name=name, email=email))

                Toast.makeText(applicationContext, "Saved", Toast.LENGTH_LONG).show()

                binding?.etName?.text?.clear()
                binding?.etEmailId?.text?.clear()
            }
        }
        else{
            Toast.makeText(this, "Please enter", Toast.LENGTH_SHORT).show()
        }
    }


    //setting up list of all data using recyclerview
    private fun setUpRecyclerView(employeesList: ArrayList<EmployeeEntity>,
    employeeDao: EmployeeDao){

        if(employeesList.isNotEmpty()) {


            //using adapter for recyclerview
            val itemAdapter = ItemAdapter(employeesList, { updateId ->
                updateRecordDialog(updateId, employeeDao)
            }) { deleteId ->
                lifecycleScope.launch {
                    employeeDao.fetchEmployeeId(deleteId).collect {
                        if (it != null) {
                            deleteRecordDialog(deleteId, employeeDao, it)
                        }
                    }
                }
            }


                binding?.rvItemsList?.layoutManager = LinearLayoutManager(this)
                binding?.rvItemsList?.adapter = itemAdapter
                binding?.rvItemsList?.visibility = View.VISIBLE
                binding?.tvNoRecordsAvailable?.visibility = View.GONE

        }
        else
        {
            binding?.rvItemsList?.visibility = View.GONE
            binding?.tvNoRecordsAvailable?.visibility = View.VISIBLE
        }
    }




    fun updateRecordDialog(id:Int, employeeDao: EmployeeDao){

        val updateDialog = Dialog(this, R.style.Theme_Dialog)

        updateDialog.setCancelable(false)

        val binding = DialogUpdateBinding.inflate(layoutInflater)
        updateDialog.setContentView(binding.root)


        //use of coroutine to fetch user from database using id and query in dao
        lifecycleScope.launch {
            employeeDao.fetchEmployeeId(id).collect{

                if(it != null)
                {
                    binding.etUpdateName.setText(it.name)
                    binding.etUpdateEmailId.setText(it.email)
                }

            }
        }



        //updating data using dailog box
        binding.tvUpdate.setOnClickListener {

            val name = binding.etUpdateName.text.toString()
            val email = binding.etUpdateEmailId.text.toString()

            if(name.isNotEmpty() && email.isNotEmpty())
            {
                // coroutine use and dao
                lifecycleScope.launch {
                    employeeDao.update(EmployeeEntity(id, name, email))

                    Toast.makeText(applicationContext, "Updated", Toast.LENGTH_LONG).show()
                    updateDialog.dismiss()
                }
            }
            else{
                Toast.makeText(this, "Please enter details", Toast.LENGTH_SHORT).show()
            }
        }

        binding.tvCancel.setOnClickListener {
            updateDialog.dismiss()
        }

        updateDialog.show()
    }


    //deleting data from database
    private fun deleteRecordDialog(id:Int, employeeDao: EmployeeDao,employee: EmployeeEntity)
    {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Delete Record")

        builder.setTitle("Delete Record")

        builder.setMessage("Are you sure you wants to delete ${employee.name}.")
        builder.setIcon(android.R.drawable.ic_dialog_alert)

        builder.setPositiveButton("Yes")
        { dialogInterface, _ ->

            lifecycleScope.launch {

                employeeDao.delete(EmployeeEntity(id))

                Toast.makeText(
                    applicationContext,
                    "Record deleted successfully.",
                    Toast.LENGTH_LONG
                ).show()

                dialogInterface.dismiss()
            }

        }

        builder.setNegativeButton("No") { dialogInterface, _ ->
            dialogInterface.dismiss()
        }

        val alertDialog: AlertDialog = builder.create()

        alertDialog.setCancelable(false)

        alertDialog.show()

    }
}
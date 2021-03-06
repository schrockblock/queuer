package com.rndapp.task_feed.activities

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.DatePicker
import com.android.volley.Response
import com.rndapp.task_feed.R
import com.rndapp.task_feed.api.*
import com.rndapp.task_feed.fragments.DayFragment
import com.rndapp.task_feed.fragments.DaysFragment
import com.rndapp.task_feed.fragments.ProjectFragment
import com.rndapp.task_feed.fragments.ProjectsFragment
import com.rndapp.task_feed.listeners.OnDayClickedListener
import com.rndapp.task_feed.listeners.OnProjectClickedListener
import com.rndapp.task_feed.models.Day
import com.rndapp.task_feed.models.Project
import com.rndapp.task_feed.models.Sprint
import com.rndapp.task_feed.models.SprintProject
import java.util.*

class SprintActivity: AppCompatActivity(), OnDayClickedListener, OnProjectClickedListener {
    var sprint: Sprint? = null
        set(value) {
            field = value
            sprintPagerAdapter?.sprint = value
            supportActionBar?.title = value?.nameFromStartDate()
        }
    private var sprintPagerAdapter: SprintPagerAdapter? = null
    private var viewPager: ViewPager? = null

    private var mTwoPane: Boolean = false

    companion object {
        val SPRINT_EXTRA = "SPRINT_EXTRA"

        val PROJECT_REQUEST = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sprint)

        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        if (findViewById(R.id.detail_container) != null) {
            mTwoPane = true
        }

        viewPager = findViewById(R.id.container) as ViewPager

        val sprintExtra = intent?.extras?.getSerializable(SPRINT_EXTRA)
        if (sprintExtra != null && sprintExtra is Sprint) {
            setupWith(sprintExtra)
        } else {
            val calendar = Calendar.getInstance()
            DatePickerDialog(this, this::sprintDateChosen, calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    fun refresh() {
        if (sprint != null) {
            val request = SprintRequest(this.sprint!!, Response.Listener { sprint ->
                this.sprint = sprint
            }, Response.ErrorListener { error ->
                error.printStackTrace()
            })
            VolleyManager.queue?.add(request)
        }
    }

    fun setupWith(sprint: Sprint?) {
        this.sprint = sprint

        sprintPagerAdapter = SprintPagerAdapter(this, this, supportFragmentManager)
        sprintPagerAdapter?.sprint = sprint

        viewPager?.adapter = sprintPagerAdapter

        val tabs = findViewById(R.id.tabs) as TabLayout

        viewPager?.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabs))
        tabs.addOnTabSelectedListener(TabLayout.ViewPagerOnTabSelectedListener(viewPager))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == PROJECT_REQUEST) {
                val id = (data?.extras?.getSerializable(ChooserActivity.PROJECT) as Project?)?.id
                if (id != null) {
                    val sp = SprintProject(id)
                    val request = CreateSprintProjectRequest(this.sprint!!.id, sp, Response.Listener { response ->
                        refresh()
                    }, Response.ErrorListener { error ->
                        error.printStackTrace()
                    })
                    VolleyManager.queue?.add(request)
                }
            }
        }
    }

    fun sprintDateChosen(datePicker: DatePicker, year: Int, month: Int, day: Int) {
        val date = GregorianCalendar(year, month, day).time
        val request = CreateSprintRequest(date, Response.Listener { sprint ->
            setupWith(sprint)
            viewPager?.setCurrentItem(1, true)
        }, Response.ErrorListener { error ->
            error.printStackTrace()
        })
        VolleyManager.queue?.add(request)
    }

    override fun onDayClicked(day: Day) {
        if (mTwoPane) {
            val fragment = DayFragment()
            fragment.sprint = sprint
            fragment.day = day
            supportFragmentManager.beginTransaction()
                    .replace(R.id.detail_container, fragment)
                    .commit()
        } else {
            val intent = Intent(this, DayActivity::class.java)
            intent.putExtra(DayActivity.SPRINT_KEY, sprint)
            intent.putExtra(DayActivity.DAY_KEY, day)
            startActivity(intent)
        }
    }

    override fun onProjectClicked(project: Project) {
        if (mTwoPane) {
            val fragment = ProjectFragment()
            fragment.project = project
            supportFragmentManager.beginTransaction()
                    .replace(R.id.detail_container, fragment)
                    .commit()
        } else {
            val intent = Intent(this, ProjectActivity::class.java)
            intent.putExtra(ProjectActivity.ARG_PROJECT, project)
            startActivity(intent)
        }
    }

    inner class SprintPagerAdapter(val dayClickedListener: OnDayClickedListener,
                                   val projectClickedListener: OnProjectClickedListener,
                                   fm: FragmentManager) : FragmentPagerAdapter(fm) {
        val dayFragment = DaysFragment.newInstance(dayClickedListener)
        val projectFragment = ProjectsFragment.newInstance(projectClickedListener)
        var sprint: Sprint? = null
            set(value) {
                field = value
                projectFragment.sprint = value
                projectFragment.refresh()
                dayFragment.sprint = value
                dayFragment.refresh()
            }

        override fun getItem(position: Int): Fragment {
            when (position) {
                0 -> return dayFragment
                1 -> return projectFragment
            }
            return dayFragment
        }

        override fun getCount(): Int {
            return 2
        }
    }
}
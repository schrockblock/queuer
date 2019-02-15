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
import android.view.ViewGroup
import android.widget.DatePicker
import com.android.volley.Response
import com.rndapp.task_feed.R
import com.rndapp.task_feed.api.*
import com.rndapp.task_feed.fragments.*
import com.rndapp.task_feed.listeners.OnDayClickedListener
import com.rndapp.task_feed.listeners.OnProjectClickedListener
import com.rndapp.task_feed.models.*
import com.rndapp.task_feed.view_models.SprintActivityViewModel
import java.io.Serializable
import java.util.*

class SprintActivity: AppCompatActivity(), OnDayClickedListener, OnProjectClickedListener {
    private var viewModel: SprintActivityViewModel? = null

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

        if (findViewById<View>(R.id.detail_container) != null) {
            mTwoPane = true
        }

        viewPager = findViewById(R.id.container) as ViewPager

        val sprintExtra = intent?.extras?.getSerializable(SPRINT_EXTRA)
        if (sprintExtra != null && sprintExtra is Sprint) {
            setupViewModel(sprintExtra.id)
            setupWith(sprintExtra)
        } else {
            val calendar = Calendar.getInstance()
            DatePickerDialog(this, this::sprintDateChosen, calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    fun refresh() {
        viewModel?.refreshDays()
        viewModel?.refreshSprintProjects()
    }

    fun setupViewModel(sprintId: Int) {
        viewModel = SprintActivityViewModel(sprintId)
        viewModel?.sprintLiveData?.observeForever(this::setupWith)
        viewModel?.refreshSprint()
    }

    fun setupWith(sprint: Sprint?) {
        supportActionBar?.title = sprint?.nameFromStartDate()
        sprintPagerAdapter = SprintPagerAdapter(this, this::onSprintProjectClicked, supportFragmentManager)

        viewPager?.adapter = sprintPagerAdapter
        sprintPagerAdapter?.notifyDataSetChanged()

        val tabs = findViewById(R.id.tabs) as TabLayout

        viewPager?.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabs))
        tabs.addOnTabSelectedListener(TabLayout.ViewPagerOnTabSelectedListener(viewPager))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == PROJECT_REQUEST) {
                val projects: List<Project> = data?.extras?.getSerializable(ChooserActivity.PROJECTS) as List<Project>
                for (project in projects) {
                    val id = project.id
                    val sp = SprintProject(projectId = id)
                    val request = CreateSprintProjectRequest(this.viewModel!!.sprintId, sp, Response.Listener { response ->
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
            setupViewModel(sprint.id)
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
            fragment.day = day
            fragment.viewModel = viewModel
            supportFragmentManager.beginTransaction()
                    .replace(R.id.detail_container, fragment)
                    .commit()
        } else {
            val intent = Intent(this, DayActivity::class.java)
            intent.putExtra(DayFragment.DAY_KEY, day)
            intent.putExtra(DayFragment.SPRINT_KEY, viewModel?.sprintId)
            intent.putExtra(DayFragment.SPRINT_PROJECTS_KEY, viewModel?.sprintProjectsLiveData?.value as Serializable)
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

    fun onSprintProjectClicked(sprintProject: SprintProject) {
        sprintProject.sprintId = viewModel!!.sprintId
        if (mTwoPane) {
            val fragment = SprintProjectFragment()
            fragment.sprintProject = sprintProject
            fragment.viewModel = viewModel
            supportFragmentManager.beginTransaction()
                    .replace(R.id.detail_container, fragment)
                    .commit()
        } else {
            val intent = Intent(this, SprintProjectActivity::class.java)
            intent.putExtra(SprintProjectActivity.ARG_PROJECT, sprintProject)
            startActivity(intent)
        }
    }

    inner class SprintPagerAdapter(val dayClickedListener: OnDayClickedListener,
                                   val projectClickedListener: (SprintProject) -> Unit,
                                   fm: FragmentManager) : FragmentPagerAdapter(fm) {
        var dayFragment: DaysFragment? = null
        var projectFragment: SprintProjectsFragment? = null

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            when (position) {
                0 -> dayFragment = DaysFragment.newInstance(dayClickedListener, viewModel!!)
                1 -> projectFragment = SprintProjectsFragment.newInstance(projectClickedListener, viewModel!!)
            }
            return super.instantiateItem(container, position)
        }

        override fun getItem(position: Int): Fragment {
            when (position) {
                0 -> {
//                    dayFragment.sprint = sprint
                    val frag = DaysFragment.newInstance(dayClickedListener, viewModel!!)
                    return frag
                }
                1 -> {
//                    projectFragment.sprint = sprint
                    val frag = SprintProjectsFragment.newInstance(projectClickedListener, viewModel!!)
                    return frag
                }
            }
            return DaysFragment.newInstance(dayClickedListener, viewModel!!)
        }

        override fun getItemId(position: Int): Long {
            return Random().nextLong()
        }

        override fun getCount(): Int {
            return 2
        }
    }
}
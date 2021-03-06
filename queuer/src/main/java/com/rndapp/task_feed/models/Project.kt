package com.rndapp.task_feed.models

import java.io.Serializable
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created with IntelliJ IDEA.
 * User: ell
 * Date: 5/22/13
 * Time: 1:39 PM
 */
data class Project @JvmOverloads constructor(var id: Int = 0, var name: String?,
                                             var createdAt: Date? = null,
                                             var updated_at: Date? = null,
                                             var color: Int,
                                             var isHidden: Boolean = false,
                                             var tasks: ArrayList<Task> = ArrayList()) : Serializable {

    override fun toString() = name ?: ""

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false

        val project = o as Project?

        return id == project!!.id
    }
}

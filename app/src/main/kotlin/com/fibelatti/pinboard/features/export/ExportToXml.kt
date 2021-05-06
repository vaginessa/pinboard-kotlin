package com.fibelatti.pinboard.features.export

import android.util.Xml
import com.fibelatti.pinboard.core.AppConfig
import com.fibelatti.pinboard.features.posts.domain.model.Post
import java.io.StringWriter

private const val NO_NAMESPACE = ""

class ExportToXml {

    fun export(posts: List<Post>) {

        val xmlSerializer = Xml.newSerializer()
        val writer = StringWriter()

        with(xmlSerializer) {
            setOutput(writer)
            startDocument(AppConfig.API_ENCODING, true)
            startTag(NO_NAMESPACE, "posts")

            for (post in posts) {
                startTag(NO_NAMESPACE, "post")

                attribute(NO_NAMESPACE, "href", post.url)
                attribute(NO_NAMESPACE, "description", post.title)
                attribute(NO_NAMESPACE, "extended", post.description)
                attribute(NO_NAMESPACE, "hash", post.hash)
                attribute(NO_NAMESPACE, "time", post.time)
                attribute(NO_NAMESPACE, "shared", post.private.toString())
                attribute(NO_NAMESPACE, "toread", post.readLater.toString())
                attribute(NO_NAMESPACE, "tag", post.tags.toString())

                endTag(NO_NAMESPACE, "post")
            }

            endTag(NO_NAMESPACE, "posts")
            endDocument()
            flush()
        }
    }
}

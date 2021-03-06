package grails.test.mixin

import grails.artefact.Artefact
import grails.persistence.Entity
import spock.lang.Specification

/**
 * @video Graeme Rocher
 */
@TestFor(VideoController)
@Mock(Video)
class RestfulControllerSpec extends Specification {

    def populateValidParams(params) {
        assert params != null
        // TODO: Populate valid properties like...
        //params["name"] = 'someValidName'
    }


    void "Test the index action returns the correct model"() {

        when:"The index action is executed"
            controller.index()

        then:"The model is correct"
            assert !model.videoList
            assert model.videoCount == 0
    }

    void "Test the create action returns the correct model"() {
        when:"The create action is executed"
            controller.create()

        then:"The model is correctly created"
            model.video != null
    }

    void "Test the save action correctly persists an instance"() {

        when:"The save action is executed with an invalid instance"
            def video = new Video(title: '')
            video.validate()
            controller.save(video)

        then:"The create view is rendered again with the correct model"
            model.video != null
            view == 'create'

        when:"The save action is executed with a valid instance"
            response.reset()
            video = new Video(title: "Game of Thrones")
//            populateValidParams(params)
            controller.save(video)

        then:"A redirect is issued to the show action"
            response.redirectedUrl == '/video/show/1'
            controller.flash.message != null
            Video.count() == 1
    }

    void "Test that the show action returns the correct model"() {
        when:"The show action is executed with a null domain"
            controller.show(null)

        then:"A 404 error is returned"
            response.status == 404

        when:"A domain instance is passed to the show action"
    //        populateValidParams(params)
            def video = new Video(title: "Game of Thrones")
            controller.show(video)


        then:"A model is populated containing the domain instance"
            model.video == video
    }

    void "Test that the edit action returns the correct model"() {
        when:"The edit action is executed with a null domain"
            controller.edit(null)

        then:"A 404 error is returned"
            response.status == 404

        when:"A domain instance is passed to the edit action"
            //        populateValidParams(params)
            def video = new Video(title: "Game of Thrones")
            controller.edit(video)


        then:"A model is populated containing the domain instance"
            model.video == video
    }


    void "Test the update action performs an update on a valid domain instance"() {
        when:"Update is called for a domain instance that doesn't exist"
            controller.update(null)

        then:"A 404 error is returned"
            status == 404

        when:"An invalid domain instance is passed to the update action"
            response.reset()
            def video = new Video(title: '')
            video.validate()
            controller.update(video)

        then:"The edit view is rendered again with the invalid instance"
            view == 'edit'
            model.video == video

        when:"A valid domain instance is passed to the update action"
            response.reset()
            //populateValidParams(params)
            video = new Video(title: 'Game of Thrones').save(flush: true)
            controller.update(video)

        then:"A redirect is issues to the show action"
            response.redirectedUrl == "/video/show/$video.id"
            flash.message != null

    }

    void "Test that the delete action deletes an instance if it exists"() {
        when:"The delete action is called for a null instance"
            controller.delete(null)

        then:"A 404 is returned"
            status == 404

        when:"A domain instance is created"
            response.reset()
            //populateValidParams(params)
            def video = new Video(title: 'Game of Thrones').save(flush: true)

        then:"It exists"
            Video.count() == 1

        when:"The domain instance is passed to the delete action"
            controller.delete(video)

        then:"The instance is deleted"
            Video.count() == 0
            response.redirectedUrl == '/video/index'
            flash.message != null

    }
}

@Entity
class Video {
    String title

    static constraints = {
        title blank:false
    }
}

import grails.transaction.*

@Transactional(readOnly = true)
@Artefact("Controller")
class VideoController {

    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        respond Video.list(params), model:[videoCount: Video.count()]
    }

    def show(Video video) {
        respond video
    }

    def create() {
        respond new Video(params)
    }

    @Transactional
    def save(Video video) {
        if(video.hasErrors()) {
            respond video.errors, view:'create' // STATUS CODE 422
        }
        else {
            video.save flush:true
            withFormat {
                html {
                    flash.message = message(code: 'default.created.message', args: [message(code: 'video.label', default: 'Video'), video.id])
                    redirect video
                }
                '*' { render status:201 }
            }
        }
    }

    def edit(Video video) {
        respond video
    }

    @Transactional
    def update(Video video) {
        if(video == null) {
            render status:404
        }
        else if(video.hasErrors()) {
            respond video.errors, view:'edit' // STATUS CODE 422
        }
        else {
            video.save flush:true
            withFormat {
                html {
                    flash.message = message(code: 'default.updated.message', args: [message(code: 'Video.label', default: 'Video'), video.id])
                    redirect video
                }
                '*'{ render status:200 }
            }
        }
    }

    @Transactional
    def delete(Video video) {
        if(video) {
            video.delete flush:true
            withFormat {
                html {
                    flash.message = message(code: 'default.deleted.message', args: [message(code: 'Video.label', default: 'Video'), video.id])
                    redirect action:"index", method:"GET"
                }
                '*'{ render status:204 } // NO CONTENT STATUS CODE  
            }
        }
        else {
            render status:404
        }
    }
}


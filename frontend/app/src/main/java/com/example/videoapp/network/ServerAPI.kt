package com.example.videoapp.network


import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ServerAPI {

    @GET("/feed")
    fun feed(
        @Query("page_size") pageSize:Int,
        @Query("page_number") pageNumber:Int,
        @Query("seed_value") seedValue:Float?
    ): Call<VideosResponse>

    @GET("/my_medias")
    fun myMedias():Call<MutableList<Media>>

    @GET("/my_profile")
    fun myProfile():Call<MyProfile>

    @GET("/my_views")
    fun myViews():Call<MutableList<Media>>

    @GET("/my_likes")
    fun myLikes():Call<MutableList<Media>>

    @GET("/my_dislikes")
    fun myDislikes():Call<MutableList<Media>>

    @GET("/video")
    fun getVideo(@Query("video_id") videoId: Int):Call<VideoResponse>

    @POST("/video")
    fun postVideo(@Body body:MultipartBody):Call<ResponseBody>

    @Multipart
    @PUT("/video")
    fun putVideo(
        @Part("title") title: RequestBody,
        @Part("description") description: RequestBody,
        @Part("visibility") visibility: RequestBody,
        @Part("video_id") videoId: RequestBody,
        @Part("tags") tags: RequestBody,
        @Part thumbnail: MultipartBody.Part
    ):Call<ResponseBody>

    @DELETE("/video")
    fun deleteVideo(@Query("video_id") videoId: Int):Call<ResponseBody>

    @GET("/subscriptions")
    fun getSubscriptions():Call<SubscriptionsResponse>

    @POST("/subscribe")
    fun subscribe(@Query("subscribed_id") subscribedId:Int):Call<ResponseBody>

    @DELETE("/subscribe")
    fun deleteSubscription( @Query("subscribed_id") subscribedId:Int):Call<ResponseBody>

    @POST("/like")
    fun like(@Query("type") type: String, @Query("is_like") isLike: Boolean, @Query("like_id") likeId: Int):Call<ResponseBody>

    @DELETE("/like")
    fun deleteLike(@Query("type") type: String,@Query("like_id") likeId: Int):Call<ResponseBody>

    @POST("/comment")
    fun comment(@Body commentRequest: CommentRequest): Call<Comment>

    @GET("/comment")
    fun getComment(@Query("comment_id") commentId: Int):Call<Comment>

    @DELETE("/comment")
    fun deleteComment(@Query("comment_id") commentId:Int):Call<ResponseBody>

    @GET("/comments/{type}/{type_id}")
    fun comments(@Path("type") type: String,@Path("type_id") typeId: Int):Call<MutableList<Comment>>

    @GET("/replies/{comment_id}")
    fun replies(@Path("comment_id") commentId: Int):Call<MutableList<Comment>>

    @GET("/files/{name}")
    fun files(@Path("name") name:String):Call<ResponseBody>

    @GET("/profile")
    fun getProfile(@Query("profile_id") profileId: Int?):Call<Profile>

    @Multipart
    @PUT("/profile/{pid}")
    fun updateProfile(
        @Part("bio") bio: String,
        @Part("password") password:String,
        @Part("pp") profilePicture:RequestBody?,
        ):Call<ResponseBody>

    @DELETE("/profile")
    fun deleteProfile():Call<ResponseBody>

    @Multipart
    @POST("/register")
    fun register(
        @Part("username") username:String,
        @Part("password") password:String,
        @Part("bio") bio:String,
        @Part("pp") profilePicture: RequestBody?
    ):Call<LoginResponse>

    @POST("/login")
    fun login(@Body loginRequest: LoginRequest):Call<LoginResponse>

    @GET("/search/{search}")
    fun search(@Path("search") text:String):Call<Array<SearchResponse>>

    @GET("/search/videos")
    fun searchVideos(
        @Query("search") text: String,
        @Query("page_size") pageSize:Int,
        @Query("page_number") pageNumber:Int,
        @Query("seed_value") seedValue:Float?):Call<VideosResponse>

    @GET("/poll")
    fun getPoll(@Query("poll_id")pollId:Int):Call<PollResponse>

    @POST("/poll")
    fun createPoll(@Body body:MultipartBody):Call<Int>
    /*
    val requestBody: MultipartBody = MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart("userId", "your_user_id")
        .addFormDataPart("name", "name")
        .addFormDataPart(
            "photo",
            photoFile.name,
            RequestBody.create("image/*".toMediaTypeOrNull(), photoFile)
        ).build()
     */
     */

     @DELETE("/poll")
     fun deletePoll(@Query("poll_id") pollId:Int):Call<ResponseBody>

     @POST("/poll/vote/{poll_id}")
     fun addPollVote(@Path("poll_id")pollId: Int, @Query("poll_option_id")pollOptionId:Int):Call<ResponseBody>

     @DELETE("poll/vote/{poll_id}")
     fun deletePollVote(@Path("poll_id")pollId: Int):Call<ResponseBody>

     @GET("/post")
     fun getPost(@Query("post_id")postId:Int):Call<PostResponse>

     @POST("/post")
     fun createPost(@Body body: MultipartBody):Call<Int>

     @DELETE("/post")
     fun deletePost(@Query("post_id") postId:Int):Call<ResponseBody>

     @GET("/video_preview")
     fun getVideoPreview(@Query("video_id")videoId:Int):Call<Video>

     @GET("/check_progress/{resource}")
     fun checkProgress(@Path("resource") videoResource:String):Call<ProgressResponse>

     @POST("/send_chunk/{resource}")
     fun sendChunk(@Path("resource") videoResource: String,@Body chunk:RequestBody):Call<ResponseBody>

    @POST("/prepare_video/{resource}")
    fun prepareVideo(@Path("resource") videoResource: String):Call<ResponseBody>
}



data class Comment(
    val id: List<Comment>,
    val postId: Long,
    val author: Author,
    val content: String,
    val published: Long,
    val likedByMe: Boolean,
    val likes: Int = 0,
)
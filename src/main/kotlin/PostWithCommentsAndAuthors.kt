data class PostWithCommentsAndAuthors(
    val post: PostWithAuthor,
    val comments: List<Comment>,
)
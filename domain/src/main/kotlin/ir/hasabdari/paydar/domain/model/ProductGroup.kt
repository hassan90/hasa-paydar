package ir.hasabdari.paydar.domain.model

data class ProductGroup(
    val id: String,
    val name: String,
    val parentId: String? = null,
    val isActive: Boolean = true,
    val storeId: String
) {
    init {
        require(name.isNotBlank()) { "نام گروه نمی‌تواند خالی باشد" }
    }

    val isMainGroup: Boolean get() = parentId == null
}

/** گره درخت — گروه + تعداد کالا + زیرگروه‌ها */
data class ProductGroupNode(
    val group: ProductGroup,
    val productCount: Int = 0,
    val children: List<ProductGroupNode> = emptyList()
) {
    val totalProductCount: Int get() = productCount + children.sumOf { it.totalProductCount }
}
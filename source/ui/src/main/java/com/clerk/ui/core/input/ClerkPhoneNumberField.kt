import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.ui.core.dimens.dp1
import com.clerk.ui.core.dimens.dp12
import com.clerk.ui.core.dimens.dp16
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.LocalClerkDesign
import com.clerk.ui.theme.LocalClerkThemeColors
import com.clerk.ui.theme.LocalComputedColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneInput(modifier: Modifier = Modifier) {
  ClerkMaterialTheme {
    val colors = LocalClerkThemeColors.current
    val computedColors = LocalComputedColors.current
    val design = LocalClerkDesign.current
    Row(
      modifier =
        Modifier.background(color = colors.background)
          .border(
            width = dp1,
            color = computedColors.inputBorder,
            shape = RoundedCornerShape(design.borderRadius),
          )
          .padding(dp16)
          .then(modifier)
    ) {
      Text("\uD83C\uDDFA\uD83C\uDDF8")
    }
  }
}

@PreviewLightDark
@Composable
private fun PreviewPhoneInput() {
  ClerkMaterialTheme {
    Box(
      modifier = Modifier.background(color = MaterialTheme.colorScheme.background).padding(dp12)
    ) {
      PhoneInput()
    }
  }
}

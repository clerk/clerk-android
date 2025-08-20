import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.clerk.ui.R
import com.clerk.ui.core.dimens.dp1
import com.clerk.ui.core.dimens.dp12
import com.clerk.ui.core.dimens.dp14
import com.clerk.ui.core.dimens.dp16
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.dimens.dp4
import com.clerk.ui.core.dimens.dp8
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.LocalClerkDesign
import com.clerk.ui.theme.LocalClerkThemeColors
import com.clerk.ui.theme.LocalComputedColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneInput(modifier: Modifier = Modifier, value: String? = null, onClick: () -> Unit) {
  var inputValue by remember { mutableStateOf(value.orEmpty()) }
  ClerkMaterialTheme {
    Row(
      modifier = Modifier.fillMaxWidth().then(modifier),
      horizontalArrangement = Arrangement.spacedBy(dp12),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      CountrySelector(onClick)
      OutlinedTextField(
        modifier = Modifier.weight(1f),
        value = inputValue,
        onValueChange = { inputValue = it },
        label = { Text("Enter your phone number", style = MaterialTheme.typography.bodyMedium) },
        singleLine = true,
      )
    }
  }
}

@Composable
private fun CountrySelector(onClick: () -> Unit, modifier: Modifier = Modifier) {
  val colors = LocalClerkThemeColors.current
  val computedColors = LocalComputedColors.current
  val design = LocalClerkDesign.current
  Box(modifier = Modifier.padding(top = dp8).heightIn(min = 56.dp).clickable { onClick() }) {
    Row(
      modifier =
        Modifier.background(color = colors.background)
          .border(
            width = dp1,
            color = computedColors.inputBorder,
            shape = RoundedCornerShape(design.borderRadius),
          )
          .padding(dp16)
          .then(modifier),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
    ) {
      Text("\uD83C\uDDFA\uD83C\uDDF8")
      Spacer(modifier = Modifier.width(dp8))
      Text("US", style = MaterialTheme.typography.bodyLarge, color = colors.foreground)
      Spacer(modifier = Modifier.width(dp14))
      Icon(
        modifier = Modifier.size(dp24),
        painter = painterResource(R.drawable.ic_chevron_down),
        contentDescription = null,
        tint = colors.mutedForeground,
      )
    }
    Text(
      modifier =
        Modifier.align(Alignment.TopStart)
          .padding(horizontal = dp12)
          .offset(y = (-7).dp)
          .background(color = colors.background)
          .padding(horizontal = dp4)
          .zIndex(1f),
      text = stringResource(R.string.country),
      color = colors.mutedForeground,
      style = MaterialTheme.typography.bodySmall,
    )
  }
}

@PreviewLightDark
@Composable
private fun PreviewPhoneInput() {
  ClerkMaterialTheme {
    Column(
      modifier =
        Modifier.background(color = MaterialTheme.colorScheme.background)
          .fillMaxWidth()
          .fillMaxSize()
          .padding(dp12),
      verticalArrangement = Arrangement.spacedBy(dp12),
    ) {
      PhoneInput(value = "+1 (301) 237 0655", onClick = {})
      PhoneInput(onClick = {})
    }
  }
}

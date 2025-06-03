---
title: unsafeMetadata
---
//[Clerk Android](../../../index.html)/[com.clerk.signup](../index.html)/[SignUp](index.html)/[unsafeMetadata](unsafe-metadata.html)



# unsafeMetadata



[androidJvm]\




@SerialName(value = &quot;unsafe_metadata&quot;)



val [unsafeMetadata](unsafe-metadata.html): JsonObject? = null



Metadata that can be read and set from the frontend. Once the sign-up is complete, the value of this field will be automatically copied to the newly created user's unsafe metadata. One common use case for this attribute is to use it to implement custom fields that can be collected during sign-up and will automatically be attached to the created User object.





# Release Notes

## 2.0.95

### Enhancements
- **update-gradle** updated gradle

## 2.0.94-1

### Enhancements
- **gradle-properties** Added/reordered google() in repositories 

## 2.0.90-4

### Bug fixes
- **EMP-11339** Updated ExpandedControllerActivity layout (outdated layout was causing a crash in reference ExpandedControllerActivity after play-services-cast-framework update).

## 2.0.89

### Bug fixes
- **EMP-11326** bumped play-services-cast-framework version to 15.0.1. This fixes app crashes in some Huawei phones that have outdated Google Player Services. Although app no longer crashes, in these phones Chromecast button still won't be visible until Google Play Services is updated/reinstalled.  


## 2.0.82

### Bug fixes
- playFrom property is now being sent inside the playbackProperties json object

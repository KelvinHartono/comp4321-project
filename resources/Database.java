package resources;

public enum Database {
  URLtoPageID, // URL<=>PageID
  PageIDtoURLInfo, // PageID <=> URL Date Size UnstemmedPageTitle
  ParentToChild, // PageID(parent) <=> PageID(child) (linked)
  ChildToParent, // PageID(child) <=> PageID(parent)
  ForwardIndex, // PageID <=> WordID freq
  WordMap, // Word <=> WordID
  WordToPage, // WordID@pageID <=> score(tf) positioninfo
  HTMLtoPage, // WordID(in html body) <=> pageIDs
  InvertedIndex // WordID(in page title) <=> pageIDs
};
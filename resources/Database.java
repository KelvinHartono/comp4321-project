package resources;

public enum Database {
  URLtoPageID, // URL<=>PageID
  PageIDtoURLInfo, // PageID <=> URL Date Size UnstemmedPageTitle
  ParentToChild, // PageID(parent) <=> PageID(child) (linked)
  ChildToParent, // PageID(child) <=> PageID(parent)
  ForwardIndex, // PageID <=> WordID freq (nowcast=2, about=5, academ=5,)
  WordToPage, // Word@pageID <=> freq positioninfos (freq@positioninfo1@positioninfo2@......)
  HTMLtoPage, // Word(in html body) <=> pageIDs
  InvertedIndex, // Word(in page title) <=> pageIDs
  PageRank, //PageID <=> PageRank scores
};
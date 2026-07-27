//generate ebay sold link
function getCompLink(row) {
  let link = "https://www.ebay.com/sch/i.html?_nkw=";
  var end = "&_sacat=0&LH_Complete=1&LH_Sold=1";
  var year = row.find(".show-year").text();
  if (year != "") {
    link += year;
    link += "+";
  }

  var publisher = row.find(".show-publisher").text();
  if (publisher != "") {
    link += publisher;
    link += "+";
  }
  var set = row.find(".show-set").text();
  if (set != "") {
    if(publisher==="Topps" && set.startsWith("Bowman")){
        link = link.slice(0, -6);
    }
    link += set;
    link += "+";
  }
  var player = row.find(".show-player").text();
  if (player != "") {
    link += player;
    link += "+";
  }
  var insert = row.find(".show-insert").text();
  if (insert != "") {
    link += insert;
    link += "+";
  }
  var parallel = row.find(".show-parallel").text();
  if (parallel != "") {
    link += parallel;
    link += "+";
  }
  /* The auto cell renders a badge rather than the raw boolean, so read the
     value off data-auto when it's there and fall back to the cell text. */
  var autoCell = row.find(".show-auto");
  var auto = autoCell.data("auto") !== undefined
    ? String(autoCell.data("auto"))
    : autoCell.text();
  if (auto == "true") {
    link += "auto";
    link += "+";
  }
  var numbered = row.find(".show-numbered").text();
  if (numbered != "") {
    let num = numbered.split("/");
    let deno = num[1];
    link += "%2F";
    link += deno;
    link += "+";
  }
  var grade = row.find(".show-grade").text();
  if (grade != "") {
    link += grade;
    link += "+";
  }

  link = link.slice(0, -1) + end;

  return link;
}

window.getCompLink = getCompLink;

/*
filedrag.js - HTML5 File Drag & Drop demonstration
Featured on SitePoint.com
Developed by Craig Buckler (@craigbuckler) of OptimalWorks.net
 */

(function() {

	// getElementById
	function $id(id) {
		return document.getElementById(id);
	}

	// initialize
	function Init() {

		$("#upload").submit(function(event){

			event.preventDefault();
			
			var xhr = new XMLHttpRequest();
			
			// file received/failed
			xhr.onreadystatechange = function(e) {
				if (xhr.readyState == 4) {
					$id("fileselect").value = '';
					//progress.className = (xhr.status == 200 ? "success" : "failure")
					if (xhr.status == 200) {
						//var response = xhr.responseText.split('$$$');						
						alert("Application(s) submitted successfully! Please check the 'Report' section.");//+response);
					} else {
						alert("Submission failed! Please check your inputs. ("+xhr.responseText+")");
					}
				}
			};

			// start upload
			xhr.open("POST", $id("upload").action, true);
			var formData = new FormData();
			var inputs = $("#upload input")
			$.each(inputs, function (i, v) {
				var name = $(v).attr("id");
				if ("fileselect"==name) {
					$.each(v.files, function (index,file){
						var filename = $(file).attr("name");
						formData.append(name,file,filename)						
					})
				} else {
					formData.append(name,v.value)
				}
			})
			xhr.send(formData);
		})

	}

	// call initialization file
	if (window.File && window.FileList && window.FileReader) {
		Init();
	}

})();
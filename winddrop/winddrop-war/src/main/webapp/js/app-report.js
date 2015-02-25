/* Builds the updated table for the member list */
function buildFileRows(files) {
	return _.template( $( "#file-tmpl" ).html(), {"files": files});
}

/* Uses JAX-RS GET to retrieve current file list */
function updateFileTable() {
   $.ajax({
	   url: "rest/files/json",
	   cache: false,
	   success: function(data) {
            $('#files').empty().append(buildFileRows(data));
       },
       error: function() {
       }
   });
}


/* Get the file template */
function getFileTemplate() {
	$.ajax({
        url: "tmpl/file.tmpl",
        dataType: "html",
        success: function( data ) {
            $( "head" ).append( data );
            updateFileTable();
        }
    });
}

$(document).ready( function() {
    $( "#container" ).show();
    //Fetches the initial member table
    getFileTemplate();
});
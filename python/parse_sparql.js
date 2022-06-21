const fs = require('fs');

// Small script to parse a CSV file of SPARQL queries, and output JSON

// cf. docs: https://github.com/RubenVerborgh/SPARQL.js
// yarn add sparqljs


function CSVToArray( strData, strDelimiter ){
    // Check to see if the delimiter is defined. If not,
    // then default to comma.
    strDelimiter = (strDelimiter || ",");

    // Create a regular expression to parse the CSV values.
    var objPattern = new RegExp(
        (
            // Delimiters.
            "(\\" + strDelimiter + "|\\r?\\n|\\r|^)" +

            // Quoted fields.
            "(?:\"([^\"]*(?:\"\"[^\"]*)*)\"|" +

            // Standard fields.
            "([^\"\\" + strDelimiter + "\\r\\n]*))"
        ),
        "gi"
        );


    // Create an array to hold our data. Give the array
    // a default empty first row.
    var arrData = [[]];

    // Create an array to hold our individual pattern
    // matching groups.
    var arrMatches = null;


    // Keep looping over the regular expression matches
    // until we can no longer find a match.
    while (arrMatches = objPattern.exec( strData )){

        // Get the delimiter that was found.
        var strMatchedDelimiter = arrMatches[ 1 ];

        // Check to see if the given delimiter has a length
        // (is not the start of string) and if it matches
        // field delimiter. If id does not, then we know
        // that this delimiter is a row delimiter.
        if (
            strMatchedDelimiter.length &&
            strMatchedDelimiter !== strDelimiter
            ){

            // Since we have reached a new row of data,
            // add an empty row to our data array.
            arrData.push( [] );

        }

        var strMatchedValue;

        // Now that we have our delimiter out of the way,
        // let's check to see which kind of value we
        // captured (quoted or unquoted).
        if (arrMatches[ 2 ]){

            // We found a quoted value. When we capture
            // this value, unescape any double quotes.
            strMatchedValue = arrMatches[ 2 ].replace(
                new RegExp( "\"\"", "g" ),
                "\""
                );

        } else {

            // We found a non-quoted value.
            strMatchedValue = arrMatches[ 3 ];

        }


        // Now that we have our value string, let's add
        // it to the data array.
        arrData[ arrData.length - 1 ].push( strMatchedValue );
    }

    // Return the parsed data.
    return( arrData );
}


// Convert the CSV to a JSON array

const data = fs.readFileSync('sample.csv', 'utf8');
const parsedQueries = CSVToArray(data, ',')

const SparqlParser = require('sparqljs').Parser;
const parser = new SparqlParser({ skipValidation: true });

const allQueries = []

// For each SPARQL query in the CSV file: try to parse it
for (const query of parsedQueries) {
    try {
        allQueries.push({
            query: query[0],
            domain: query[1],
            client: query[2],
            date: query[3],
            parsed: parser.parse(query[0]),
        })
    } catch (err) {
        allQueries.push({
            query: query[0],
            domain: query[1],
            client: query[2],
            date: query[3],
            // parsed: parser.parse(query[0]),
        })
    }
}

console.log(allQueries)

fs.writeFileSync('parsed_queries.json', JSON.stringify(allQueries));
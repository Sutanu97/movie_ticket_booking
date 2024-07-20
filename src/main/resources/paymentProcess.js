function proceedWithBooking(){
    console.log("In method proceedWithBooking");
    event.preventDefault();
    const username = "sutanu";
    const password = "root1!";
    base64Credentials = btoa(username+":"+password);
    let params = {
        "show" : {
            "pkMovieShowId" : 17,
            "movie" : {
                "pkMovieId":1,
                "name":"Oppenheimer"
            },
            "multiplex":{
                "name":"PVR Manisquare",
                "address":"Mani Square Mall, 164/1, Maniktala Main Rd, near Eastern Metropolitan Bypass Road, Kadapara, Phool Bagan, Road, Kolkata, West Bengal 700054"
            },
            "screen":{
                "multiplexScreenPk":{
                    "screenId": 1
                }
            },
            "startTime":"2024-06-03 17:30:00",
            "endTime":"2024-06-03 20:15:00"
        },
        "seats" : "A1 A2 A3"
    }
    let xhr = new XMLHttpRequest();
    xhr.open('POST','http://localhost:8080/shows/book', true);
    xhr.setRequestHeader('Authorization', 'Basic ' + base64Credentials);
    xhr.setRequestHeader('Content-type','application/json');
    xhr.onreadystatechange = function(){
        if(xhr.status === 200 && xhr.readyState === 4){
            console.log("Received success");
            console.log("XHR response : ", xhr.response);
            console.log("printing done");
        }else{
            console.error("error occured", xhr.response);
        }
    }
    xhr.send(JSON.stringify(params));
}
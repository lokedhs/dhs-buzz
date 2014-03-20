<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
    <head>
        <title>Simple jsp page</title>
    </head>
    <body>
        <form action="RequestToken" method="post">
            <div>
                <label>
                    Username:
                    <input type="text" size="20" name="username">
                </label>
            </div>
            <div>
                <label>
                    Password:
                    <input type="password" size="20" name="password">
                </label>
            </div>
            <div>
                <input type="submit" value="Send">
            </div>
        </form>
    </body>
</html>

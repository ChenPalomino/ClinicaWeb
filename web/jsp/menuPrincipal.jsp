<%-- El menú principal ahora es el dashboard (/panel). Se mantiene este archivo
     para que los enlaces antiguos sigan funcionando. --%>
<% response.sendRedirect(request.getContextPath() + "/panel"); %>

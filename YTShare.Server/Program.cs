using Microsoft.AspNetCore.Mvc;
using System.Diagnostics;
using System.Net;

var builder = WebApplication.CreateBuilder(args);

// Add services to the container.

builder.Services.AddControllers();

var app = builder.Build();

// Configure the HTTP request pipeline.

app.UseAuthorization();

app.MapGet("/GetIpAddress", () => Dns.GetHostAddresses(Dns.GetHostName()).FirstOrDefault(ip => ip.AddressFamily == System.Net.Sockets.AddressFamily.InterNetwork)?.ToString());

app.MapGet("/Share", ([FromQuery] string link) =>
{
    Process.Start(new ProcessStartInfo
    {
        FileName = $"https://{link}",
        UseShellExecute = true
    });
    return $"Browser open with link:{link}";
});

app.Run();

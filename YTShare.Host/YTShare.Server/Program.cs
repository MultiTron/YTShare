using Microsoft.AspNetCore.Mvc;
using System.Diagnostics;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddEndpointsApiExplorer();
builder.Host.UseWindowsService();
builder.WebHost.UseUrls("http://0.0.0.0:7296");

// Add services to the container.

var app = builder.Build();

// Configure the HTTP request pipeline.

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